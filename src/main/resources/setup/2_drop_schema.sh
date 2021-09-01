#!/bin/bash

psql postgresql://postgres:postgres@localhost:5432/ordering_system -f 2_drop_schema.sql