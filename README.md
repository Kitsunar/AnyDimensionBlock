# Any Dimension Block

Mod NeoForge 1.21.1 — Crée des portails vers des dimensions remplies de n'importe quel bloc.

## Craft de la Baguette Dimensionnelle

```
_ _ B
_ B _
S _ _
```
- **B** = Blaze Rod
- **S** = Nether Star

## Utilisation

1. Construis un cadre rectangulaire creux de blocs **identiques et solides** (minimum **4 blocs de large × 5 blocs de haut** total, bords inclus — soit un intérieur 2×3, comme un portail Nether).
2. Clic droit sur **n'importe quel bloc du cadre** avec la Baguette Dimensionnelle.
3. L'intérieur se remplit de blocs de portail violets.
4. Rentre dans le portail pour te téléporter dans une dimension **entièrement remplie** de ce bloc.

## Dans la dimension

- Une **bulle d'air** est créée à ton point d'arrivée (tu ne suffoqueras pas).
- Un **portail de retour** est construit automatiquement.
- **Aucun mob** ne peut spawner dans ces dimensions.
- Une **couche de bedrock** en Y minimum empêche de tomber dans le vide.
- Chaque bloc = une dimension unique et persistante entre les sessions.

## Blocs supportés

Tous les blocs solides vanilla et moddés. Les blocs liquides, l'air et le bedrock sont exclus.

## Dimensions créées

Les dimensions sont stockées dans le world save et persistent entre les redémarrages.
Elles sont identifiées par `anydimensionblock:dim_<namespace>_<blockname>`.

## Compilation

```bash
./gradlew build
```
Le .jar se trouve dans `build/libs/`.

## Compatibilité

- Minecraft 1.21.1
- NeoForge 21.1.86+
- Compatible avec les blocs de mods tiers
