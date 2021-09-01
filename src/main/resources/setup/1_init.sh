#!/bin/bash

psql postgresql://postgres:postgres@127.0.0.1:5432/postgres -f 1_init.sql
