#!/bin/bash
cat << 'INNEREOF' > replacement.txt
            // Interactive Bead Ring Area
            val currentStyle = tasbeehBeadStyles.getOrElse(selectedBeadStyle) { tasbeehBeadStyles[0] }
            
            fun incrementWithSound() {
                // Haptic and Sound
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (soundEnabled) {
                    toneGenerator?.startTone(android.media.ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE, 40)
                }
                viewModel.incrementTasbeeh { }
            }

            TasbeehRingView(
                currentStyle = currentStyle,
                onBeadClick = { incrementWithSound() }
            )

            Text(
                text = "Tap anywhere on the ring to count\n(Haptic & Sound on tap)",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
INNEREOF
