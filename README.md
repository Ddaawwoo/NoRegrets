# NoRegrets

Mobilní webová aplikace, která před večírkem aktivuje časový zámek na vybrané kontakty. Pomáhá zabránit impulzivnímu volání a psaní zpráv, kterých by člověk později litoval.

## Funkce

- chráněné kontakty s telefonem a poznámkou
- časový zámek až do zvoleného data a času
- živý odpočet a offline provoz
- zámek uložený pouze v zařízení
- střízlivostní kontrola před předčasným odemknutím
- instalace jako PWA na domovskou obrazovku

## Důležité omezení

Běžná webová aplikace nemůže blokovat systémovou aplikaci Telefon, SMS ani jiné komunikátory. NoRegrets proto blokuje akce uvnitř aplikace a funguje jako behaviorální pojistka. Pro skutečné systémové blokování je nutná nativní aplikace pro Android s příslušnými oprávněními.

## Spuštění

Otevřete `index.html` přes lokální HTTP server, například `npx serve .`. Pro instalaci PWA je potřeba HTTPS nebo localhost.

