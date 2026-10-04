import os
import uuid
import logging
import numpy as np
import cv2
from typing import List, Tuple, Dict, Any, Optional
from app.utils.image_utils import crop_and_save_face

logger = logging.getLogger("face_service")

class PersonIndex:
    def __init__(self):
        # Maps person_id (str) -> List of L2-normalized numpy arrays
        self.persons: Dict[str, List[np.ndarray]] = {}

    def clear(self):
        self.persons.clear()

    def find_match(self, embedding: np.ndarray, threshold: float) -> Tuple[Optional[str], Optional[float]]:
        best_person_id = None
        best_sim = -1.0

        for person_id, embeddings_list in self.persons.items():
            for reg_emb in embeddings_list:
                sim = float(np.dot(embedding, reg_emb))
                if sim > best_sim:
                    best_sim = sim
                    best_person_id = person_id

        if best_person_id is not None and best_sim >= threshold:
            return best_person_id, round(best_sim, 4)

        return None, None

    def register_embedding(self, person_id: str, embedding: np.ndarray):
        if person_id not in self.persons:
            self.persons[person_id] = []
        self.persons[person_id].append(embedding)


class FaceService:
    _instance = None

    def __init__(self):
        self.yunet_detector = None
        self.insightface_app = None
        self.cascade_classifier = None
        self.sface_recognizer = None
        self.person_index = PersonIndex()
        self.initialized = False

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    @staticmethod
    def get_match_threshold() -> float:
        threshold_str = os.getenv("FACE_MATCH_THRESHOLD", "0.60")
        try:
            return float(threshold_str)
        except ValueError:
            return 0.60

    def initialize(self):
        if self.initialized:
            return

        logger.info("Initializing Face Service (Detector + Recognizer)...")
        
        # 1. Initialize YuNet ONNX Face Detector
        yunet_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../models/face_detection_yunet_2023mar.onnx"))
        if os.path.exists(yunet_path):
            try:
                self.yunet_detector = cv2.FaceDetectorYN.create(
                    model=yunet_path,
                    config="",
                    input_size=(300, 300),
                    score_threshold=0.5,
                    nms_threshold=0.3,
                    top_k=5000
                )
                logger.info("YuNet ONNX Face Detector loaded successfully.")
            except Exception as e:
                logger.warning(f"Failed to initialize YuNet detector: {e}")

        # 2. Try loading InsightFace if model files exist locally
        try:
            insightface_dir = os.path.expanduser("~/.insightface/models/buffalo_l")
            if os.path.exists(insightface_dir):
                import insightface
                from insightface.app import FaceAnalysis
                app = FaceAnalysis(name='buffalo_l', allowed_modules=['detection'], providers=['CPUExecutionProvider'])
                app.prepare(ctx_id=0, det_size=(640, 640))
                self.insightface_app = app
                logger.info("InsightFace FaceAnalysis model loaded.")
        except Exception as e:
            logger.info(f"InsightFace model not active: {e}")

        # 3. OpenCV Haar Cascade Classifier (fallback)
        try:
            cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
            if os.path.exists(cascade_path):
                self.cascade_classifier = cv2.CascadeClassifier(cascade_path)
                logger.info("OpenCV Haar Cascade Classifier loaded.")
        except Exception as e:
            logger.warning(f"Failed to initialize Haar Cascade: {e}")

        # 4. Initialize SFace ONNX Face Recognizer
        sface_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "../../models/face_recognition_sface_2021dec.onnx"))
        if os.path.exists(sface_path):
            try:
                self.sface_recognizer = cv2.FaceRecognizerSF.create(
                    model=sface_path,
                    config=""
                )
                logger.info("SFace ONNX Face Recognizer loaded successfully.")
            except Exception as e:
                logger.warning(f"Failed to initialize SFace recognizer: {e}")

        self.initialized = True

    def detect_and_crop_faces(self, img: np.ndarray) -> List[Dict[str, Any]]:
        if not self.initialized:
            self.initialize()

        img_h, img_w = img.shape[0], img.shape[1]
        detected_faces = []
        bboxes: List[Tuple[int, int, int, int]] = []

        # Strategy 1: YuNet ONNX
        if self.yunet_detector is not None:
            try:
                self.yunet_detector.setInputSize((img_w, img_h))
                _, faces = self.yunet_detector.detect(img)
                if faces is not None:
                    for face in faces:
                        x, y, w, h = int(face[0]), int(face[1]), int(face[2]), int(face[3])
                        bboxes.append((x, y, x + w, y + h))
            except Exception as e:
                logger.error(f"YuNet detection error: {e}")

        # Strategy 2: InsightFace
        if len(bboxes) == 0 and self.insightface_app is not None:
            try:
                faces = self.insightface_app.get(img)
                for face in faces:
                    bbox = face.bbox
                    bboxes.append((int(bbox[0]), int(bbox[1]), int(bbox[2]), int(bbox[3])))
            except Exception as e:
                logger.error(f"InsightFace detection error: {e}")

        # Strategy 3: Haar Cascade
        if len(bboxes) == 0 and self.cascade_classifier is not None:
            try:
                gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
                gray = cv2.equalizeHist(gray)
                faces = self.cascade_classifier.detectMultiScale(
                    gray,
                    scaleFactor=1.1,
                    minNeighbors=4,
                    minSize=(30, 30)
                )
                for (x, y, w, h) in faces:
                    bboxes.append((int(x), int(y), int(x + w), int(y + h)))
            except Exception as e:
                logger.error(f"Haar Cascade detection error: {e}")

        for (x1, y1, x2, y2) in bboxes:
            x1_clamped = max(0, min(img_w - 1, x1))
            y1_clamped = max(0, min(img_h - 1, y1))
            x2_clamped = max(x1_clamped + 1, min(img_w, x2))
            y2_clamped = max(y1_clamped + 1, min(img_h, y2))

            face_id, bbox_dict, face_image_path = crop_and_save_face(
                img=img,
                bbox=(x1_clamped, y1_clamped, x2_clamped, y2_clamped)
            )

            detected_faces.append({
                "face_id": face_id,
                "bounding_box": bbox_dict,
                "face_image_path": face_image_path,
                "crop_coords": (x1_clamped, y1_clamped, x2_clamped, y2_clamped)
            })

        return detected_faces

    def extract_face_embedding(self, img: np.ndarray, crop_coords: Tuple[int, int, int, int]) -> np.ndarray:
        if not self.initialized:
            self.initialize()

        x1, y1, x2, y2 = crop_coords
        cropped = img[y1:y2, x1:x2]

        if cropped.size == 0:
            dummy = np.random.randn(128).astype(np.float32)
            return dummy / np.linalg.norm(dummy)

        resized = cv2.resize(cropped, (112, 112))

        if self.sface_recognizer is not None:
            feature = self.sface_recognizer.feature(resized)[0]
            norm = np.linalg.norm(feature)
            if norm > 0:
                feature = feature / norm
            return feature.astype(np.float32)
        else:
            hsv = cv2.cvtColor(resized, cv2.COLOR_BGR2HSV)
            hist = cv2.calcHist([hsv], [0, 1], None, [16, 8], [0, 180, 0, 256]).flatten()
            norm = np.linalg.norm(hist)
            if norm > 0:
                hist = hist / norm
            return hist.astype(np.float32)

    def identify_faces(self, img: np.ndarray) -> List[Dict[str, Any]]:
        detected_faces = self.detect_and_crop_faces(img)
        threshold = self.get_match_threshold()

        results = []

        for face in detected_faces:
            face_id = face["face_id"]
            crop_coords = face["crop_coords"]

            embedding = self.extract_face_embedding(img, crop_coords)

            matched_person_id, similarity = self.person_index.find_match(embedding, threshold)

            if matched_person_id is not None:
                status = "MATCHED"
                person_id = matched_person_id
                self.person_index.register_embedding(person_id, embedding)
            else:
                status = "NEW_PERSON"
                person_id = str(uuid.uuid4())
                similarity = None
                self.person_index.register_embedding(person_id, embedding)

            results.append({
                "face_id": face_id,
                "person_id": person_id,
                "status": status,
                "similarity": similarity,
                "face_image_path": face["face_image_path"],
                "embedding": embedding.tolist()
            })

        return results
