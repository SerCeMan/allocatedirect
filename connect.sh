#!/usr/bin/env bash

ssh -i ./allocatedirect.pem -L 1100:localhost:1100 ubuntu@18.205.104.117
# scp ubuntu@18.205.104.117:/tmp/g1/\* .
