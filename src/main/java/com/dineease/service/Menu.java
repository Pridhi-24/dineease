package com.dineease.service;

import java.util.*;

/** Static menu data (a real system would keep this in the database). */
public final class Menu {
    public static class Item {
        public final String cat, name, desc; public final int price;
        Item(String c, String n, String d, int p) { cat = c; name = n; desc = d; price = p; }
    }
    public static final List<String> CATS = List.of("Starters", "Main Course", "Desserts", "Beverages");
    public static final Map<String, String> GRADIENT = Map.of(
        "Starters", "#2f6a58,#183B32", "Main Course", "#7b4a2a,#2d1a0e", "Desserts", "#6b3f4a,#2a1018", "Beverages", "#2d5a6a,#10303a");
    private static final List<Item> ITEMS = List.of(
        new Item("Starters", "Burrata & Heirloom Tomato", "Creamy burrata, basil oil, aged balsamic", 420),
        new Item("Starters", "Wild Mushroom Velout\u00e9", "Truffle cream, toasted sourdough", 360),
        new Item("Starters", "Charred Corn Ribs", "Smoked paprika, lime, cotija", 320),
        new Item("Starters", "Crispy Calamari", "Lemon aioli, charred chilli, micro herbs", 380),
        new Item("Starters", "Smoked Beetroot Carpaccio", "Whipped goat cheese, candied walnuts", 340),
        new Item("Starters", "Paneer Tikka Crostini", "Mint chutney, pickled red onion", 310),
        new Item("Main Course", "Saffron Risotto", "Arborio, saffron, parmesan crisp", 640),
        new Item("Main Course", "Herb-Crusted Lamb Rack", "Rosemary jus, roasted root vegetables", 1180),
        new Item("Main Course", "Pan-Seared Sea Bass", "Lemon beurre blanc, fennel salad", 980),
        new Item("Main Course", "Wild Mushroom Gnocchi", "Brown butter, crisp sage, shaved parmesan", 720),
        new Item("Main Course", "Tandoori Chicken Supreme", "Saffron yoghurt, charred greens, pan jus", 820),
        new Item("Main Course", "Slow-Braised Short Rib", "Red wine glaze, pomme pur\u00e9e", 1260),
        new Item("Desserts", "Dark Chocolate Fondant", "Warm centre, vanilla bean ice cream", 390),
        new Item("Desserts", "Lemon Tart", "Crisp shell, torched meringue", 340),
        new Item("Desserts", "Pistachio Panna Cotta", "Rose syrup, crushed pistachio", 360),
        new Item("Desserts", "Tiramisu Classico", "Espresso mascarpone, cocoa dust", 360),
        new Item("Desserts", "Saffron Rasmalai Cheesecake", "Pistachio crumb, dried rose petals", 380),
        new Item("Desserts", "Seasonal Fruit Pavlova", "Whipped cream, warm berry compote", 350),
        new Item("Beverages", "Mint & Cucumber Cooler", "Fresh mint, lime, sparkling water", 210),
        new Item("Beverages", "Single-Origin Pour-Over", "Seasonal beans, brewed to order", 240),
        new Item("Beverages", "Hibiscus Spritz", "Hibiscus, orange peel, tonic", 260),
        new Item("Beverages", "Classic Masala Chai", "Slow-brewed with ginger and cardamom", 160),
        new Item("Beverages", "Fresh Mango Lassi", "Chilled yoghurt, saffron, pistachio", 200),
        new Item("Beverages", "Sparkling Elderflower", "Lime zest, chilled soda", 230));

    public static List<Item> of(String cat) { return ITEMS.stream().filter(i -> i.cat.equals(cat)).toList(); }
    private Menu() {}
}
