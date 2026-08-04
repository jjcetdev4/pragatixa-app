#!/bin/bash
# EC2 Bootstrap / Deployment Script Updates

# 1. Create the access-logs directory
mkdir -p /opt/pragatix/access-logs

# 2. Change ownership to the user the app runs as (e.g., ec2-user)
chown -R ec2-user:ec2-user /opt/pragatix/access-logs

# (Optional) If using an environment file instead of application.yml:
# echo "server.tomcat.accesslog.enabled=true" >> /etc/pragatix-backend.env
# echo "server.tomcat.accesslog.directory=/opt/pragatix/access-logs" >> /etc/pragatix-backend.env
# echo "server.tomcat.accesslog.prefix=access_log" >> /etc/pragatix-backend.env
# echo "server.tomcat.accesslog.suffix=.log" >> /etc/pragatix-backend.env
# echo "server.tomcat.accesslog.pattern=%h %l %u %t \"%r\" %s %b %D" >> /etc/pragatix-backend.env
