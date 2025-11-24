#!/bin/bash
cd /home/kavia/workspace/code-generation/android-tv-game-booster-46019-46028/game_booster_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

