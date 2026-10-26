package com.dbsmp.jlai;

import java.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class JLAIPlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final String TITLE = ChatColor.DARK_PURPLE + "JLAI" + ChatColor.GRAY + " - Just Looking At Items";
    private static final int PAGE_SIZE=45, PREV=45, SEARCH=47, INFO=49, RECIPE=51, NEXT=53;
    private final List<Material> items = new ArrayList<Material>();

    public void onEnable() {
        for (Material m : Material.values()) if (m != null && m != Material.AIR && m.isItem()) items.add(m);
        Collections.sort(items, new Comparator<Material>() {
            public int compare(Material a, Material b) { return a.name().compareTo(b.name()); }
        });
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("jlai").setExecutor(this);
        getCommand("jlai").setTabCompleter(this);
        getLogger().info("JLAI enabled. Indexed " + items.size() + " item types.");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("JLAI can only be used in-game."); return true; }
        Player p=(Player)sender;
        if (args.length>0 && args[0].equalsIgnoreCase("search")) {
            if (args.length<2) { p.sendMessage(ChatColor.YELLOW+"Usage: /jlai search <name>"); return true; }
            open(p,0,join(args,1)); return true;
        }
        if (args.length>0 && args[0].equalsIgnoreCase("recipe")) {
            if (args.length<2) { p.sendMessage(ChatColor.YELLOW+"Usage: /jlai recipe <item name>"); return true; }
            recipe(p,join(args,1)); return true;
        }
        if (args.length>0 && args[0].equalsIgnoreCase("compass")) {
            p.getInventory().addItem(compass());
            p.sendMessage(ChatColor.LIGHT_PURPLE+"JLAI compass added. Right-click it to browse items.");
            return true;
        }
        open(p,0,""); return true;
    }

    private String join(String[] a,int start) { StringBuilder b=new StringBuilder(); for(int i=start;i<a.length;i++){if(i>start)b.append(' ');b.append(a[i]);} return b.toString(); }

    private void open(Player p,int page,String query) {
        List<Material> list=filter(query);
        int max=Math.max(0,(list.size()-1)/PAGE_SIZE); page=Math.max(0,Math.min(page,max));
        Inventory inv=Bukkit.createInventory(null,54,TITLE+" ["+(page+1)+"/"+(max+1)+"]");
        for(int i=0;i<PAGE_SIZE && page*PAGE_SIZE+i<list.size();i++){
            Material m=list.get(page*PAGE_SIZE+i); ItemStack it=new ItemStack(m);
            ItemMeta meta=it.getItemMeta();
            if(meta!=null){meta.setDisplayName(ChatColor.YELLOW+pretty(m.name())); meta.setLore(Arrays.asList(ChatColor.GRAY+"ID: "+m.name(),ChatColor.GRAY+"Data value: 0 (legacy item)",ChatColor.GREEN+"Click for item info"));it.setItemMeta(meta);}
            inv.setItem(i,it);
        }
        inv.setItem(PREV, named(Material.ARROW,ChatColor.YELLOW+"Previous page",Arrays.asList(ChatColor.GRAY+"Go back one page")));
        inv.setItem(SEARCH,named(Material.COMPASS,ChatColor.AQUA+"Search items",Arrays.asList(ChatColor.GRAY+"Use /jlai search <name>")));
        inv.setItem(INFO,named(Material.BOOK,ChatColor.LIGHT_PURPLE+"JLAI Item Browser",Arrays.asList(ChatColor.GRAY+"Items found: "+list.size(),ChatColor.GRAY+"Search: "+(query.isEmpty()?"none":query),ChatColor.GRAY+"Read-only: no items are given")));
        inv.setItem(RECIPE,named(Material.WORKBENCH,ChatColor.GREEN+"Recipe lookup",Arrays.asList(ChatColor.GRAY+"Use /jlai recipe <item name>")));
        inv.setItem(NEXT,named(Material.ARROW,ChatColor.YELLOW+"Next page",Arrays.asList(ChatColor.GRAY+"Go forward one page")));
        p.openInventory(inv);
    }

    private ItemStack named(Material m,String name,List<String> lore){ItemStack i=new ItemStack(m);ItemMeta x=i.getItemMeta();if(x!=null){x.setDisplayName(name);x.setLore(lore);i.setItemMeta(x);}return i;}
    private List<Material> filter(String q){List<Material> r=new ArrayList<Material>();String s=q==null?"":q.trim().toLowerCase(Locale.ENGLISH).replace(' ','_');for(Material m:items)if(s.isEmpty()||m.name().toLowerCase(Locale.ENGLISH).contains(s))r.add(m);return r;}
    private int page(String title){try{int a=title.lastIndexOf('['),b=title.lastIndexOf('/');return Integer.parseInt(title.substring(a+1,b))-1;}catch(Exception e){return 0;}}

    @EventHandler public void click(InventoryClickEvent e){
        if(e.getView()==null||e.getView().getTitle()==null||!e.getView().getTitle().startsWith(ChatColor.DARK_PURPLE+"JLAI"))return;
        e.setCancelled(true); if(!(e.getWhoClicked() instanceof Player))return;
        int slot=e.getRawSlot(); if(slot<0||slot>=e.getView().getTopInventory().getSize())return;
        Player p=(Player)e.getWhoClicked(); String title=e.getView().getTitle();
        if(slot==PREV||slot==NEXT){open(p,page(title)+(slot==NEXT?1:-1),"");return;}
        ItemStack clicked=e.getCurrentItem();if(clicked==null||clicked.getType()==Material.AIR)return;
        if(slot<PAGE_SIZE){Material m=clicked.getType();p.sendMessage(ChatColor.LIGHT_PURPLE+"[JLAI] "+ChatColor.YELLOW+pretty(m.name())+ChatColor.GRAY+" | ID: "+m.name()+" | Data: 0");p.sendMessage(ChatColor.GRAY+"Recipe lookup: /jlai recipe "+m.name().toLowerCase(Locale.ENGLISH));}
        else if(slot==SEARCH){p.closeInventory();p.sendMessage(ChatColor.AQUA+"Search: /jlai search <name>");}
        else if(slot==RECIPE){p.closeInventory();p.sendMessage(ChatColor.GREEN+"Recipe lookup: /jlai recipe <item name>");}
    }

    @EventHandler public void compassUse(PlayerInteractEvent e){
        ItemStack it=e.getItem();if(it==null||it.getType()!=Material.COMPASS||it.getItemMeta()==null)return;
        ItemMeta meta=it.getItemMeta();if(!meta.hasDisplayName()||!ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase("JLAI Item Browser"))return;
        if(e.getAction()!=Action.RIGHT_CLICK_AIR&&e.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        e.setCancelled(true);open(e.getPlayer(),0,"");
    }

    private ItemStack compass(){return named(Material.COMPASS,ChatColor.LIGHT_PURPLE+"JLAI Item Browser",Arrays.asList(ChatColor.GRAY+"Right-click to browse items",ChatColor.GRAY+"Read-only item reference"));}
    private void recipe(Player p,String query){
        String q=query.trim().toLowerCase(Locale.ENGLISH).replace(' ','_');Material target=null;
        for(Material m:items)if(m.name().equalsIgnoreCase(q)){target=m;break;}
        if(target==null)for(Material m:items)if(m.name().toLowerCase(Locale.ENGLISH).contains(q)){target=m;break;}
        if(target==null){p.sendMessage(ChatColor.RED+"[JLAI] No item matched '"+query+"'.");return;}
        p.sendMessage(ChatColor.GREEN+"[JLAI] Recipe lookup for "+ChatColor.YELLOW+pretty(target.name()));
        boolean found=false;
        try{Iterator<Recipe> it=Bukkit.recipeIterator();while(it.hasNext()){Recipe r=it.next();if(r.getResult()!=null&&r.getResult().getType()==target){found=true;break;}}}catch(Exception ignored){}
        p.sendMessage(found?ChatColor.GRAY+"A registered crafting recipe exists for this item.":ChatColor.GRAY+"No registered crafting recipe found; it may be smelted, dropped, or not craftable.");
        p.sendMessage(ChatColor.GRAY+"Full ingredient-grid display is not included in v1.0. JLAI never gives items.");
    }
    private String pretty(String s){StringBuilder b=new StringBuilder();for(String w:s.toLowerCase(Locale.ENGLISH).split("_")){if(b.length()>0)b.append(' ');if(w.length()>0)b.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));}return b.toString();}
    public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){if(args.length==1)return Arrays.asList("search","recipe","compass");return Collections.emptyList();}
}
