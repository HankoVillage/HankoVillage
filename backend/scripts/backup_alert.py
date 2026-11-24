"""Send a short backup alert to the OCI Notifications topic (email subscribers).

deploy/backup.sh runs it inside the api container when a step fails:

    docker compose exec -T api python -m scripts.backup_alert "dump upload"

The message holds only the failed step names and the time. No personal data, no paths.
Send a test message with:  python -m scripts.backup_alert --test
"""

import socket
import sys
from datetime import datetime, timedelta, timezone

from app.config import settings

KST = timezone(timedelta(hours=9))


def main() -> int:
    topic_id = (settings.backup_alert_topic_id or "").strip()
    if not topic_id:
        print("alert: BACKUP_ALERT_TOPIC_ID is not set, skipped")
        return 1

    now = datetime.now(KST).strftime("%Y-%m-%d %H:%M KST")
    if sys.argv[1:] == ["--test"]:
        title = "[HankoVillage] backup alert test"
        body = f"Test message from {socket.gethostname()} at {now}. No action needed."
    else:
        failed = " ".join(sys.argv[1:]) or "unknown"
        title = "[HankoVillage] DB backup failed"
        body = (
            f"Failed step(s): {failed}\n"
            f"Time: {now}\n"
            "Check ~/backups/backup.log on the server and run deploy/backup.sh again after fixing it."
        )

    try:
        import oci

        signer = oci.auth.signers.InstancePrincipalsSecurityTokenSigner()
        client = oci.ons.NotificationDataPlaneClient(config={"region": settings.oci_region}, signer=signer)
        client.publish_message(topic_id, oci.ons.models.MessageDetails(title=title, body=body))
    except Exception as exc:
        print(f"alert: {type(exc).__name__}")
        return 1

    print("alert: sent")
    return 0


if __name__ == "__main__":
    sys.exit(main())
