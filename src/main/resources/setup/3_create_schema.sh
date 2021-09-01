#!/bin/bash

psql postgresql://ordering_system:ordering_system@localhost:5432/ordering_system -f 3_create_schema.sql