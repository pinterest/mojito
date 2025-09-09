from pinci import AutomaticRetry
from pinci import Email
from pinci import Workflow
from pinci.commands import Sh
from pinci.helpers import get_env_var
from pinci.jobs import Job
from pinci.plugins import Plugin

build_url = get_env_var("BUILDKITE_BUILD_URL")

Workflow(
    nimbus_project="mojito",
    jobs=[
        Job(
            label=":docker: Mojito branch cleanup",
            commands=[
                Sh(
                    """
                    # Delete all fully translated branches older than 3 months
                    mojito branch-delete -r shopify -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r pinboard -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r webapp -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r ios -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r android -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r optimus -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r shuffles_android -t true -nr  '(?!^master$).*' -cb 3M -d false
                    mojito branch-delete -r optimization_workbench -t true -nr  '(?!^master$).*' -cb 3M -d false

                    # Delete all branches older than 6 months (abandoned PRs)
                    mojito branch-delete -r shopify -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r pinboard -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r webapp -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r ios -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r android -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r optimus -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r shuffles_android -nr  '(?!^master$).*' -cb 6M -d false
                    mojito branch-delete -r optimization_workbench -nr  '(?!^master$).*' -cb 6M -d false
                    """
                ).log_group(":docker: Mojito branch cleanup"),
            ],
        )
        .plugins(
            Plugin(
                "docker",
                "v5.12.0",
                {
                    "environment": [
                        "BUILDKITE_AGENT_ACCESS_TOKEN",
                        "BUILDKITE_BUILD_ID",
                        "BUILDKITE_BRANCH",
                        "BUILDKITE_BUILD_URL",
                        "BUILDKITE_JOB_ID",
                        "BUILDKITE_PIPELINE_PROVIDER",
                        "BUILDKITE_PULL_REQUEST_BASE_BRANCH",
                        "BUILDKITE_PULL_REQUEST",
                        "BUILDKITE_PIPELINE_SLUG",
                        "GIT_MERGE_BASE",
                        "MOJITO_HOST=mojito.pinadmin.com",
                        "MOJITO_SKIP_GIT_BLAME='True'",
                        "FAILURE_SLACK_NOTIFICATION_CHANNEL=C9W7ZUKRS",
                        "FAILURE_URL={}".format(build_url),
                        "L10N_PROXY_ENABLED=true",
                        "L10N_PROXY_HOST=localhost",
                        "L10N_PROXY_PORT=19193",
                        "L10N_RESTTEMPLATE_USESLOGINAUTHENTICATION=false",
                    ],
                    "image": "998131032990.dkr.ecr.us-east-1.amazonaws.com/mojito-cli:mojito_prod_cli",
                    "network": "host",
                    "always-pull": True,
                    "pull-retries": 5,
                    "propagate-uid-gid": True,
                    "volumes": [
                        "/mnt/git-repos/:/mnt/git-repos",
                        "/usr/bin/knox:/usr/bin/knox:ro",
                        "/var/lib/normandie:/var/lib/normandie:ro,rslave",
                        "/var/lib/knox:/var/lib/knox",
                    ],
                },
            )
        )
        .retry(automatic=AutomaticRetry().limit(3))
        .timeout_in_minutes(30)
    ],
    notify=[
        Email("growth-copytune-low-email-buildkite@pinterest.pagerduty.com").when(
            'build.state == "failing"'
        ),
    ],
        )
