#!/bin/bash
sed -i 's/Icons\.Filled\.ArrowBack/Icons\.AutoMirrored\.Filled\.ArrowBack/g' app/src/main/java/com/example/ui/BooksScreen.kt
sed -i 's/Icons\.Filled\.KeyboardArrowRight/Icons\.AutoMirrored\.Filled\.KeyboardArrowRight/g' app/src/main/java/com/example/ui/BooksScreen.kt
sed -i 's/Icons\.Filled\.FormatAlignLeft/Icons\.AutoMirrored\.Filled\.FormatAlignLeft/g' app/src/main/java/com/example/ui/CardsMakerScreen.kt
sed -i 's/Icons\.Filled\.FormatAlignRight/Icons\.AutoMirrored\.Filled\.FormatAlignRight/g' app/src/main/java/com/example/ui/CardsMakerScreen.kt
sed -i 's/Icons\.Filled\.Undo/Icons\.AutoMirrored\.Filled\.Undo/g' app/src/main/java/com/example/ui/CardsMakerScreen.kt
sed -i 's/Icons\.Filled\.ArrowBack/Icons\.AutoMirrored\.Filled\.ArrowBack/g' app/src/main/java/com/example/ui/DuasScreen.kt
sed -i 's/Icons\.Filled\.Sort/Icons\.AutoMirrored\.Filled\.Sort/g' app/src/main/java/com/example/ui/DuasScreen.kt
sed -i 's/Icons\.Filled\.ArrowForward/Icons\.AutoMirrored\.Filled\.ArrowForward/g' app/src/main/java/com/example/ui/DuasScreen.kt
sed -i 's/Icons\.Filled\.MenuBook/Icons\.AutoMirrored\.Filled\.MenuBook/g' app/src/main/java/com/example/ui/DuasScreen.kt
sed -i 's/Icons\.Filled\.LibraryBooks/Icons\.AutoMirrored\.Filled\.LibraryBooks/g' app/src/main/java/com/example/ui/DuasScreen.kt
sed -i 's/Icons\.Filled\.FormatListBulleted/Icons\.AutoMirrored\.Filled\.FormatListBulleted/g' app/src/main/java/com/example/ui/DuasScreen.kt
echo "Done"
