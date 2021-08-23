#!/bin/bash

psql -U postgres -h localhost -d ordering_system -f 2_drop_schema.sql