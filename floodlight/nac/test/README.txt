test_cli.py is a Tallac Blacklist REST API test tool.

Supported commands:
- getUsers [--mac MAC]|[--ip IP]
- changeUser MAC --state GUEST|UNAUTHENTICATED|AUTHENTICATED --details String
- getLogs
- authGuest [--ip IP]
- authClient [--ip IP] --username USERNAME -- password PASSWORD
