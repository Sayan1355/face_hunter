from pydantic import BaseModel
from typing import List, Optional

class BoundingBox(BaseModel):
    x: int
    y: int
    width: int
    height: int

class DetectedFace(BaseModel):
    face_id: str
    bounding_box: BoundingBox
    face_image_path: str

class FaceDetectionResponse(BaseModel):
    image_width: int
    image_height: int
    faces_detected: int
    faces: List[DetectedFace]

class FaceEmbeddingItem(BaseModel):
    face_id: str
    embedding_dimension: int
    embedding: List[float]

class FaceEmbeddingResponse(BaseModel):
    faces_detected: int
    faces: List[FaceEmbeddingItem]

class FaceMatchItem(BaseModel):
    face_id: str
    person_id: str
    status: str  # "MATCHED" or "NEW_PERSON"
    similarity: Optional[float] = None
    face_image_path: Optional[str] = None
    embedding: Optional[List[float]] = None

class FaceIdentifyResponse(BaseModel):
    faces_detected: int
    matches: List[FaceMatchItem]
