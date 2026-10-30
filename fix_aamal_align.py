import re

with open("app/src/main/java/com/example/ui/AamalScreen.kt", "r") as f:
    content = f.read()

old_urdu = """                                            if (isUrdu && act.urduTranslation != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = act.urduTranslation,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.DarkGray
                                                )
                                            }"""
new_urdu = """                                            if (isUrdu && act.urduTranslation != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = act.urduTranslation,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Right
                                                )
                                            }"""
content = content.replace(old_urdu, new_urdu)

with open("app/src/main/java/com/example/ui/AamalScreen.kt", "w") as f:
    f.write(content)
