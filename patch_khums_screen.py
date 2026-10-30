import re

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "r") as f:
    content = f.read()

# I will use a series of string replacements or write a new composable for the live prices

content = content.replace('CalcTool("donations", if (currentLang == "ur") "عطیات کی تاریخ" else "Donation History", if (currentLang == "ur") "اپنے عطیات کی تفصیلات دیکھیں" else "View your donation logs.", Icons.Default.HistoryEdu),', 
                          'CalcTool("donations", if (currentLang == "ur") "فنانس کی تاریخ" else "Finance History", if (currentLang == "ur") "اپنے تمام مالی معاملات دیکھیں" else "View your complete finance logs.", Icons.Default.HistoryEdu),\n        CalcTool("finance_settings", if (currentLang == "ur") "فنانس کی ترتیبات" else "Finance Settings", if (currentLang == "ur") "کرنسی، مرجع، اور قیمتوں کی ترتیبات" else "Settings for Currency, Marja and Live Prices.", Icons.Default.Settings),')

content = content.replace('"donations" -> if (currentLang == "ur") "عطیات کی تاریخ" else "Donation History"',
                          '"donations" -> if (currentLang == "ur") "فنانس کی تاریخ" else "Finance History"\n                            "finance_settings" -> if (currentLang == "ur") "فنانس کی ترتیبات" else "Finance Settings"')

content = content.replace('"donations" -> DonationHistoryTab(currentLang, currencySymbol, viewModel)',
                          '"donations" -> FinanceHistoryTab(currentLang, currencySymbol, viewModel)\n                    "finance_settings" -> FinanceSettingsTab(currentLang, currencySymbol, viewModel)')

with open("app/src/main/java/com/example/ui/KhumsScreen.kt", "w") as f:
    f.write(content)

