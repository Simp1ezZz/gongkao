"""Download external images and upload to MinIO."""

import hashlib
import httpx
import asyncio
from io import BytesIO
from minio import Minio
from urllib.parse import urlparse
from app.core.config import settings


def get_minio_client() -> Minio:
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )


def ensure_bucket(client: Minio):
    if not client.bucket_exists(settings.minio_bucket):
        client.make_bucket(settings.minio_bucket)


def image_url_to_key(url: str) -> str:
    ext = ".png"
    parsed = urlparse(url)
    path = parsed.path
    if "." in path.split("/")[-1]:
        ext = "." + path.split("/")[-1].rsplit(".", 1)[1]
        if ext not in (".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp", ".svg"):
            ext = ".png"
    hash_str = hashlib.md5(url.encode()).hexdigest()[:16]
    return f"questions/{hash_str}{ext}"


async def download_image(url: str) -> tuple[str, bytes, str] | None:
    try:
        async with httpx.AsyncClient(timeout=30, follow_redirects=True) as client:
            resp = await client.get(url)
            if resp.status_code == 200:
                content_type = resp.headers.get("content-type", "image/png")
                return url, resp.content, content_type
    except Exception:
        pass
    return None


async def download_and_upload_images(
    image_urls: list[str],
    minio_client: Minio,
) -> dict[str, str]:
    ensure_bucket(minio_client)
    url_to_key = {}

    tasks = [download_image(url) for url in image_urls]
    results = await asyncio.gather(*tasks)

    for result in results:
        if result is None:
            continue
        url, data, content_type = result
        key = image_url_to_key(url)

        try:
            minio_client.stat_object(settings.minio_bucket, key)
            url_to_key[url] = key
            continue
        except Exception:
            pass

        minio_client.put_object(
            settings.minio_bucket,
            key,
            BytesIO(data),
            length=len(data),
            content_type=content_type,
        )
        url_to_key[url] = key

    return url_to_key


def replace_urls_in_html(html: str, url_map: dict[str, str]) -> str:
    for original_url, key in url_map.items():
        serve_url = f"/api/files/serve/{key}"
        html = html.replace(original_url, serve_url)
    return html
