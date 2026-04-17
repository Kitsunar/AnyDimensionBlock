ServerEvents.recipes(event => {
    console.info('--- FORCE RECIPE LOAD ---')

    // Recette simple : 1 terre = 1 diamant (JUSTE POUR TESTER)
    event.shapeless('minecraft:diamond', ['minecraft:dirt']).id('kubejs:test_dirt_to_diamond')

    // Ta recette sans forme
    event.shapeless(
        'anydimensionblock:dimension_wand',
        [
            'minecraft:ender_pearl',
            'minecraft:blaze_rod',
            'minecraft:nether_star'
        ]
    ).id('anydimensionblock:wand_force_shapeless')
})