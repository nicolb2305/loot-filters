package com.lootfilters;

import net.runelite.api.ChatMessageType;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;

import static com.lootfilters.util.TextUtil.quote;
import static java.util.Collections.emptyList;
import static net.runelite.client.util.ImageUtil.loadImageResource;

public class LootFiltersPanel extends PluginPanel {
    private final LootFiltersPlugin plugin;

    public LootFiltersPanel(LootFiltersPlugin plugin) throws Exception {
        this.plugin = plugin;
        render();
    }

    private void render() throws Exception {
        var placeholder = loadImageResource(this.getClass(), "/com/lootfilters/icons/Placeholder.png");
        var main = new JPanel();

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        var top = new JPanel();
        top.setLayout(new FlowLayout(FlowLayout.LEFT));

        var label = new JLabel("Active filter:");
        top.add(label);

        var filterSelect = new JComboBox<String>();

        var addButton = new JButton("", new ImageIcon(placeholder));
        addButton.addActionListener(it -> {
            String source;
            try {
                source = getClipboard();
            } catch (Exception e) {
                return;
            }

            LootFilter newFilter;
            try {
                newFilter = LootFilter.fromSource(source);
            } catch (Exception e) {
                plugin.getClientThread().invoke(() -> {
                    plugin.getClient().addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                            "Failed to load filter from clipboard: " + e.getMessage(), "");
                });
                return;
            }

            for (int i = 0; i < filterSelect.getItemCount(); ++i) {
                if (filterSelect.getItemAt(i).equals(newFilter.getName())) {
                    plugin.getClientThread().invoke(() -> {
                        plugin.getClient().addChatMessage(ChatMessageType.GAMEMESSAGE, "",
                                "Cannot import filter: a filter with name " + newFilter.getName() + " already exists", "");
                    });
                    return;
                }
            }

            filterSelect.addItem(newFilter.getName());

            var newCfg = new ArrayList<>(plugin.getUserFilters());
            newCfg.add(source);
            plugin.setUserFilters(newCfg);
        });

        addButton.setBorder(null);
        top.add(addButton);

        var deleteButton = new JButton("", new ImageIcon(placeholder));
        deleteButton.setBorder(null);
        deleteButton.addActionListener(it -> {
            var index = filterSelect.getSelectedIndex();
            if (plugin.getUserFilters().isEmpty() || index == -1) {
                return;
            }

            filterSelect.removeItemAt(index);
            filterSelect.setSelectedIndex(-1);

            var newCfg = new ArrayList<>(plugin.getUserFilters());
            newCfg.remove(index);
            plugin.setUserFilters(newCfg);
            plugin.setUserFilterIndex(-1);
        });
        top.add(deleteButton);

        main.add(top);

        var filters = plugin.getUserFilters();
        for (var filter : filters) {
            filterSelect.addItem(LootFilter.fromSource(filter).getName());
        }

        var index = plugin.getUserFilterIndex();
        if (index <= filters.size()-1) {
            filterSelect.setSelectedIndex(index);
        }

        filterSelect.addActionListener(it -> {
            var newIndex = filterSelect.getSelectedIndex();
            if (newIndex == -1) {
                return;
            }

            plugin.setUserFilterIndex(newIndex);
            plugin.addPluginChatMessage("Loading filter " + quote((String) filterSelect.getSelectedItem()));
        });
        main.add(filterSelect);

        var deleteAll = new JButton("", new ImageIcon(placeholder));
        deleteAll.addActionListener(it -> {
            filterSelect.removeAllItems();
            filterSelect.setSelectedIndex(-1);

            plugin.setUserFilters(emptyList());
            plugin.setUserFilterIndex(-1);
        });
        main.add(deleteAll);

        add(main);
    }

    public static String getClipboard() throws Exception {
        return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
    }
}
