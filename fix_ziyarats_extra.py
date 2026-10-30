import re

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "r") as f:
    content = f.read()

old_block = """                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Dialog"""

new_block = """                                    )
                                }
                            }
                        }
                    }
                }
                
                // Extra Supplication Ziyarats (that are not already in ZiyaratItems)
                val existingIds = allZiyarats.map { it.id }
                val mappedIds = allZiyarats.map { z -> 
                    when (z.id) {
                        "ziyarat_ashura" -> "ashura"
                        "ziyarat_fatimah" -> "ziyarat_fatima"
                        "ziyarat_yasin" -> "ziyarat_aal_yaseen"
                        else -> z.id
                    }
                }
                val extraZiyarats = supplicationZiyarats.filter { dua -> 
                    !existingIds.contains(dua.id) && !mappedIds.contains(dua.id)
                }
                
                if (extraZiyarats.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (languageCode == "ur") "مزید زیارات" else "More Ziyarats",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F2E27),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    items(extraZiyarats, key = { it.id }) { dua ->
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFD4AF37).copy(alpha = 0.22f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(3.dp, RoundedCornerShape(24.dp))
                                .clickable { 
                                    saveRecentSearch(searchQuery.trim())
                                    onOpenDua?.invoke(dua)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(0xFF0F2E27).copy(alpha = 0.06f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color(0xFF0F2E27),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (languageCode == "ur") (dua.titleUr ?: dua.title) else dua.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F2E27)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (languageCode == "ur") dua.category else dua.description,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Read",
                                    tint = Color(0xFFD4AF37),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Screen Dialog"""

content = content.replace(old_block, new_block)

with open("app/src/main/java/com/example/ui/ZiyaratsScreen.kt", "w") as f:
    f.write(content)
