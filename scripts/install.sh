#!/usr/bin/env bash
host=${host}
user=${user}

scp -i "~/.ssh/Core.pem" -o StrictHostKeyChecking=no -r solar-moon-payments.service $user@$host:~/
ssh -i "~/.ssh/Core.pem" -t $user@$host -o StrictHostKeyChecking=no "sudo mv ~/solar-moon-payments.service /lib/systemd/system"
ssh -i "~/.ssh/Core.pem" -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl daemon-reload"
ssh -i "~/.ssh/Core.pem" -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl enable solar-moon-payments.service"
ssh -i "~/.ssh/Core.pem" -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl start solar-moon-payments.service"