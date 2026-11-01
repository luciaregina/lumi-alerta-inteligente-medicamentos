# Roteiro de QA de acessibilidade

Testes automáticos: `./gradlew connectedDebugAndroidTest` (emulador ou celular ligado).
O roteiro abaixo é manual, com **TalkBack** ligado (Configurações → Acessibilidade → TalkBack).

## Em todas as telas
- [ ] Todo botão e campo é lido com nome claro (sem "botão sem rótulo")
- [ ] Títulos são anunciados como "título" e dá para navegar por eles
- [ ] Alvos de toque têm pelo menos 48 dp (72 dp com "Botões maiores")
- [ ] Status nunca depende só de cor (estoque: "Acabando!", "Atenção", "Tudo certo")
- [ ] Foco visível (borda) ao navegar com teclado ou switch

## Por perfil do cadastro
| Perfil | Como montar | O que conferir |
|---|---|---|
| Padrão | idade 30, nenhuma opção | Resumo mostra "Perfil padrão" |
| 60+ | idade 65 | Letra 125%, botões maiores, modo simples |
| Visão | marcar Visão | Letra 150%, amarelo sobre preto, alarme falado em pt-BR |
| Daltonismo | marcar Daltonismo | Alto contraste; avisos com texto |
| Audição | marcar Audição | Alarme pisca (1×/s) e vibra; não pisca com "Remover animações" ligado |
| Motora | marcar Dificuldade motora | Botões e áreas de toque maiores |
| Memória | marcar Memória/leitura | Modo simples ligado |

## Ajustes do sistema
- [ ] Fonte do sistema no máximo + letra 200% no app: nada cortado, telas rolam
- [ ] Tema escuro do sistema com tema "Automático"
- [ ] Alarme com tela bloqueada: abre em tela cheia, foco inicial em "Tomei"
