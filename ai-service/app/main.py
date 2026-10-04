from fastapi import FastAPI
from contextlib import asynccontextmanager
import logging
from app.api.face_api import router as face_router
from app.services.face_service import FaceService

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("main")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Model initialization on startup
    logger.info("Service starting up. Loading face detection model...")
    FaceService.get_instance().initialize()
    yield
    logger.info("Service shutting down...")

app = FastAPI(
    title="Privacy-First Photo Intelligence AI Microservice",
    version="1.0.0",
    lifespan=lifespan
)

app.include_router(face_router)

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "ai-service"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8001, reload=True)
