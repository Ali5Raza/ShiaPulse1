import re

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "r") as f:
    content = f.read()

old_duas = """                loadedDuas.filter { recIds.contains(it.id) }.take(2).forEach {
                    recommendedList.add(SmartRec(
                        id = it.id,
                        type = "duas",
                        title = it.title,
                        titleUr = it.title,
                        titleAr = it.titleAr ?: it.title,
                        category = it.category,
                        descEng = it.description
                    ))
                }"""

new_duas = """                loadedDuas.filter { recIds.contains(it.id) }.take(2).forEach {
                    recommendedList.add(SmartRec(
                        id = it.id,
                        type = "duas",
                        title = it.title,
                        titleUr = it.title,
                        titleAr = it.title,
                        category = it.category,
                        descEng = it.description
                    ))
                }"""
content = content.replace(old_duas, new_duas)

old_aamal = """                loadedAamal.filter { recAamalIds.contains(it.id) }.take(2).forEach {
                    recommendedList.add(SmartRec(
                        id = it.id,
                        type = "aamal",
                        title = it.title,
                        titleUr = it.titleUr ?: it.title,
                        titleAr = it.titleAr ?: it.title,
                        category = it.category,
                        descEng = it.description
                    ))
                }"""
                
new_aamal = """                loadedAamal.filter { recAamalIds.contains(it.id) }.take(2).forEach {
                    recommendedList.add(SmartRec(
                        id = it.id,
                        type = "aamal",
                        title = it.title,
                        titleUr = it.titleUr ?: it.title,
                        titleAr = it.titleAr ?: it.title,
                        category = it.category,
                        descEng = it.description
                    ))
                }"""
content = content.replace(old_aamal, new_aamal)

with open("app/src/main/java/com/example/ui/DashboardScreen.kt", "w") as f:
    f.write(content)
