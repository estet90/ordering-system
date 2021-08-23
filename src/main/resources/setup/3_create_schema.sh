#!/bin/bash

psql -U ordering_system -h localhost -d ordering_system -f 3_create_schema.sql