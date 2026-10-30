#!/bin/bash
cat app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/com/example/api/GeminiClient.class | grep -a "ContentType"
