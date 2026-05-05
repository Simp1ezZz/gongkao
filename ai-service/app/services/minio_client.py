import io
import uuid
import logging
from minio import Minio
from minio.http import Method
from app.core.config import settings

logger = logging.getLogger(__name__)


def get_minio_client() -> Minio:
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=False,
    )


def ensure_bucket(client: Minio):
    if not client.bucket_exists(settings.minio_bucket):
        client.make_bucket(settings.minio_bucket)
        logger.info("Created MinIO bucket: %s", settings.minio_bucket)


def upload_image(image_data: bytes, filename: str, session_id: str) -> str:
    """Upload image bytes to MinIO, return the object key."""
    client = get_minio_client()
    ensure_bucket(client)
    ext = filename.rsplit(".", 1)[-1] if "." in filename else "png"
    object_key = f"questions/temp/{session_id}/{uuid.uuid4().hex[:8]}.{ext}"
    client.put_object(
        settings.minio_bucket,
        object_key,
        io.BytesIO(image_data),
        length=len(image_data),
        content_type=f"image/{ext}",
    )
    return object_key


def get_presigned_url(object_key: str) -> str:
    client = get_minio_client()
    return client.get_presigned_url(
        Method.GET,
        settings.minio_bucket,
        object_key,
        expires=3600,
    )
