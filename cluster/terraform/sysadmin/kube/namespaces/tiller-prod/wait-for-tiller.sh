#!/bin/bash

for i in {1..20}
do
  echo "Attempt $i to check tiller is up."
  sleep 1
  availableReplicas=$(kubectl -n tiller-prod get deployment tiller-deploy '-o=jsonpath={.status.availableReplicas}')
  if [[ "$availableReplicas" -eq "1" ]]; then
    echo "Tiller is up."
    exit 0;
  fi
done
