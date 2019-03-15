#!/bin/bash

gradle build
docker build -t yti-terminology-api .
