"""Upload one DB backup file, read from stdin, to the backup bucket.

deploy/backup.sh runs it inside the api container, which already has the OCI SDK and the
instance principal:

    docker compose exec -T api python -m scripts.backup_upload db/daily-20261125T190000Z.dump < file

The server may only create and list objects in the backup bucket (no read, overwrite or delete),
so a stolen server cannot remove or change old backups. The bucket's lifecycle rule deletes
backups after 14 days.

Output is one short line for the backup log. Errors print only the exception type, so the log
never holds request URLs or the Object Storage namespace.
"""

import base64
import hashlib
import sys

from app.config import settings

KEY_PREFIX = "db/"


def main() -> int:
    if len(sys.argv) != 2 or not sys.argv[1].startswith(KEY_PREFIX):
        print("usage: python -m scripts.backup_upload db/<file name> < backup file", file=sys.stderr)
        return 2
    key = sys.argv[1]

    data = sys.stdin.buffer.read()
    if not data:
        print("upload: empty input")
        return 1
    # Base64 of the MD5 digest, the form Object Storage uses for Content-MD5 and in object listings.
    md5 = base64.b64encode(hashlib.md5(data).digest()).decode()

    try:
        import oci

        signer = oci.auth.signers.InstancePrincipalsSecurityTokenSigner()
        client = oci.object_storage.ObjectStorageClient(config={"region": settings.oci_region}, signer=signer)
        namespace = client.get_namespace().data
        client.put_object(
            namespace,
            settings.oci_backup_bucket,
            key,
            data,
            content_md5=md5,  # Object Storage rejects the upload if the bytes it received differ
            content_type="application/octet-stream",
            if_none_match="*",  # never replace an existing backup with the same name
        )
        listed = client.list_objects(
            namespace, settings.oci_backup_bucket, prefix=key, fields="name,size,md5"
        ).data.objects
    except Exception as exc:
        print(f"upload: {type(exc).__name__}")
        return 1

    # Check the stored copy through the listing (the server has no read permission).
    stored = next((obj for obj in listed if obj.name == key), None)
    if stored is None or stored.size != len(data) or stored.md5 != md5:
        print("upload: verification failed")
        return 1

    print(f"upload: {key} {len(data)} bytes, size and md5 verified")
    return 0


if __name__ == "__main__":
    sys.exit(main())
