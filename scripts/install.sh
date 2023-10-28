#!/usr/bin/env bash
host=${host}
user=${user}

scp -o StrictHostKeyChecking=no -r generation-meter.service $user@$host:~/
ssh -t $user@$host -o StrictHostKeyChecking=no "sudo mv ~/solar-moon-payments.service /lib/systemd/system"
ssh -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl daemon-reload"
ssh -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl enable solar-moon-payments.service"
ssh -t $user@$host -o StrictHostKeyChecking=no "sudo systemctl start solar-moon-payments.service"