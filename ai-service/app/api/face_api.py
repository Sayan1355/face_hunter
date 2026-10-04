from fastapi import APIRouter, UploadFile, File, HTTPException, status
from pydantic import BaseModel
from typing import List, Optional
import numpy as np
import logging
from app.models.face_models import (
    FaceDetectionResponse,
    DetectedFace,
    BoundingBox,
    FaceEmbeddingResponse,
    FaceEmbeddingItem,
    FaceIdentifyResponse,
    FaceMatchItem
)
from app.utils.image_utils import safe_decode_image
from app.services.face_service import FaceService

logger = logging.getLogger("face_api")

router = APIRouter(prefix="/api/ai", tags=["face-ai"])

class RegisterEmbeddingRequest(BaseModel):
    person_id: str
    embedding: List[float]

@router.post("/detect-faces", response_model=FaceDetectionResponse)
async def detect_faces(file: UploadFile = File(...)):
    if not file or not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No image file provided"
        )
    
    try:
        contents = await file.read()
        if not contents:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty image file provided"
            )

        img = safe_decode_image(contents)
        if img is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or unsupported image format"
            )

        height, width = img.shape[0], img.shape[1]

        face_service = FaceService.get_instance()
        detected_faces_data = face_service.detect_and_crop_faces(img)

        faces = [
            DetectedFace(
                face_id=item["face_id"],
                bounding_box=BoundingBox(**item["bounding_box"]),
                face_image_path=item["face_image_path"]
            )
            for item in detected_faces_data
        ]

        return FaceDetectionResponse(
            image_width=width,
            image_height=height,
            faces_detected=len(faces),
            faces=faces
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error processing image in detect_faces endpoint: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to process image file"
        )


@router.post("/embedding", response_model=FaceEmbeddingResponse)
async def generate_embeddings(file: UploadFile = File(...)):
    if not file or not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No image file provided"
        )

    try:
        contents = await file.read()
        if not contents:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty image file provided"
            )

        img = safe_decode_image(contents)
        if img is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or unsupported image format"
            )

        face_service = FaceService.get_instance()
        detected_faces = face_service.detect_and_crop_faces(img)

        embedding_items = []
        for face in detected_faces:
            crop_coords = face["crop_coords"]
            emb_vector = face_service.extract_face_embedding(img, crop_coords)
            
            embedding_items.append(
                FaceEmbeddingItem(
                    face_id=face["face_id"],
                    embedding_dimension=len(emb_vector),
                    embedding=emb_vector.tolist()
                )
            )

        return FaceEmbeddingResponse(
            faces_detected=len(embedding_items),
            faces=embedding_items
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error processing image in embedding endpoint: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to process image file"
        )


@router.post("/identify", response_model=FaceIdentifyResponse)
async def identify_faces(file: UploadFile = File(...)):
    if not file or not file.filename:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No image file provided"
        )

    try:
        contents = await file.read()
        if not contents:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty image file provided"
            )

        img = safe_decode_image(contents)
        if img is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or unsupported image format"
            )

        face_service = FaceService.get_instance()
        matches_data = face_service.identify_faces(img)

        matches = [
            FaceMatchItem(
                face_id=item["face_id"],
                person_id=item["person_id"],
                status=item["status"],
                similarity=item["similarity"],
                face_image_path=item.get("face_image_path"),
                embedding=item.get("embedding")
            )
            for item in matches_data
        ]

        return FaceIdentifyResponse(
            faces_detected=len(matches),
            matches=matches
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error processing image in identify endpoint: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to process image file"
        )


@router.post("/register-embedding")
async def register_embedding(body: RegisterEmbeddingRequest):
    try:
        arr = np.array(body.embedding, dtype=np.float32)
        norm = np.linalg.norm(arr)
        if norm > 0:
            arr = arr / norm
        FaceService.get_instance().person_index.register_embedding(body.person_id, arr)
        return {"status": "ok", "person_id": body.person_id}
    except Exception as e:
        logger.error(f"Error registering embedding: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to register embedding"
        )
