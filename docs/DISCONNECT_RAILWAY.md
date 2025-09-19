# Disconnecting from Railway

This project no longer uses Railway. Follow this checklist to fully disconnect the repository and your account from Railway.

1) Unlink GitHub repository in Railway
- Railway Dashboard → Project → Settings → GitHub Integration → Disconnect or Unlink

2) Stop and remove services/deployments
- Project → Environments or Deployments: Stop any running deployments
- Remove services created for this repo (web, workers, cron, etc.)

3) Remove variables/secrets in Railway
- Project → Variables/Secrets: Delete any variables, tokens, or secrets
- If you used custom domains: remove the domain and clean DNS records

4) Optional: Export data then delete project
- If there are databases, export data first (pg_dump, etc.)
- Project → Settings → Delete Project (optional if fully migrating away)

5) Remove Railway GitHub App authorization
- GitHub → Your profile → Settings → Applications → Installed GitHub Apps or Authorized OAuth Apps → Railway → Uninstall/Remove

6) Remove repository hooks and deploy keys (if any)
- GitHub Repo → Settings → Webhooks: remove any hook pointing to Railway
- GitHub Repo → Settings → Deploy keys: remove Railway keys if listed

7) Remove repository or org secrets referencing Railway
- GitHub Repo → Settings → Secrets and variables → Actions: delete RAILWAY_* secrets if present
- GitHub Organization → Settings → Secrets and variables: remove any org-level RAILWAY_* secrets if present

8) Verify disconnection
- Push a trivial commit and confirm Railway no longer deploys
- Confirm Railway project shows no linked repository and no running services
- Confirm domains and DNS no longer reference Railway endpoints

Notes
- This repository contains no Railway configuration files or workflows.
- Releases are now delivered via GitHub Releases. See the Windows installer guide at docs/INSTALLER_WINDOWS.md.