package com.c4g7.relay.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.c4g7.relay.util.Rect;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigTest {
	private static final String VELVET = """
		{
		  "version": 1,
		  "settings": {"enabled": true, "draggable": true, "resizeable": false, "globalChatLimit": 300,
		    "globalBackgroundColor": -2147483648, "globalShowTimestamp": true, "globalTimestampFormat": "HH:mm"},
		  "windows": [
		    {"x": 0, "y": 3.75, "width": 311, "height": 150, "horizontalAnchor": "LEFT", "verticalAnchor": "BOTTOM",
		     "boundsPosition": "INSIDE", "focusedTab": 0, "visibility": "ON_NEW_MESSAGE",
		     "tabs": [{"uniqueId": "0493e746-79f1-43de-a794-16e995a1f878", "index": 0, "type": "SERVER", "name": "", "global": true, "filters": []}]},
		    {"x": 0.15, "y": 3.46, "width": 330, "height": 141, "horizontalAnchor": "LEFT", "verticalAnchor": "TOP",
		     "boundsPosition": "INSIDE", "focusedTab": 0, "visibility": "ALWAYS",
		     "tabs": [{"uniqueId": "f0185e02-d645-4490-8333-ff22608dbf4b", "index": 0, "type": "CUSTOM", "name": "Lifecycles", "global": true,
		       "filters": [{"id": "cef853b1-3627-4217-8622-449902dd29ce", "name": "Conduit Lifecycles", "include": ["[ℹ] ⌞Conduit⌝ ᐅ Service"],
		         "exclude": [], "shouldPlaySound": true, "keepOutOfMain": true, "caseSensitive": false, "advanced": true,
		         "includeRegEx": "(?is).*Service.*", "excludeRegEx": ""}]}]}
		  ]
		}
		""";

	@Test
	void importsVelvetChatWindowsTabsAndFilters() {
		RelayConfig config = VelvetChatImport.read(VELVET);
		assertEquals(2, config.windows.size());
		assertFalse(config.behaviour.resizable);
		assertEquals(300, config.global.lineLimit);
		assertEquals("HH:mm", config.global.timestampFormat);
		assertTrue(config.global.timestamps);
		assertEquals(0x80000000, config.global.backgroundColor);

		WindowConfig main = config.windows.get(0);
		assertTrue(main.tabs.getFirst().isMain());
		assertEquals(Geometry.Vertical.BOTTOM, main.geometry.vertical);
		assertEquals(0.0375F, main.geometry.y, 1e-4);
		assertEquals(311, main.geometry.width);

		WindowConfig second = config.windows.get(1);
		assertEquals(Visibility.ALWAYS, second.visibility);
		TabConfig lifecycles = second.tabs.getFirst();
		assertEquals("Lifecycles", lifecycles.name);
		assertEquals(lifecycles.id, second.activeTab);
		FilterConfig filter = lifecycles.filters.getFirst();
		assertTrue(filter.regex());
		assertTrue(filter.sound);
		assertEquals(FilterConfig.RegexMatch.WHOLE, filter.regexMatch);
		assertEquals("(?is).*Service.*", filter.includeRegex);
	}

	@Test
	void roundTripsThroughDiskAndImportsOnlyOnce(@TempDir Path directory) throws Exception {
		Path velvet = directory.resolve("chat.json");
		Files.writeString(velvet, VELVET);
		ConfigStore store = new ConfigStore(directory.resolve("relay"));
		RelayConfig first = store.load(velvet);
		first.windows.getFirst().locked = true;
		first.global.markerColor = 0xFF123456;
		store.save(first);
		assertTrue(Files.readString(store.file()).contains("\"#FF123456\""));

		Files.writeString(velvet, "{\"windows\": []}");
		RelayConfig second = store.load(velvet);
		assertEquals(2, second.windows.size());
		assertTrue(second.windows.getFirst().locked);
		assertEquals(0xFF123456, second.global.markerColor);
	}

	@Test
	void brokenConfigIsSetAsideAndDefaultsLoad(@TempDir Path directory) throws Exception {
		ConfigStore store = new ConfigStore(directory);
		Files.writeString(store.file(), "{ not json");
		RelayConfig config = store.load(null);
		assertEquals(1, config.windows.size());
		assertTrue(config.windows.getFirst().tabs.getFirst().isMain());
		assertTrue(Files.exists(directory.resolve("relay.json.broken")));
	}

	@Test
	void onlyOneMainTabSurvives() {
		RelayConfig config = new RelayConfig();
		config.windows.add(new WindowConfig(new Geometry(), new TabConfig(TabConfig.Kind.MAIN, "a")));
		config.windows.add(new WindowConfig(new Geometry(), new TabConfig(TabConfig.Kind.MAIN, "b")));
		config.sanitize();
		assertEquals(1, config.allTabs().stream().filter(TabConfig::isMain).count());
	}

	@Test
	void geometryRoundTripsOnAnyScreenSize() {
		Geometry geometry = new Geometry();
		Rect placed = new Rect(500, 40, 700, 120);
		geometry.store(placed, 800, 480);
		assertEquals(Geometry.Horizontal.RIGHT, geometry.horizontal);
		assertEquals(Geometry.Vertical.TOP, geometry.vertical);
		assertEquals(placed, geometry.resolve(800, 480));
		Rect bigger = geometry.resolve(1600, 960);
		assertEquals(1600 - 200, bigger.right());
		assertEquals(200, bigger.width());
	}

	@Test
	void hexColoursParse() {
		assertEquals(0xFFFF0000, HexColor.parse("#F00", 0));
		assertEquals(0xFF5A8CFF, HexColor.parse("5a8cff", 0));
		assertEquals(0x80000000, HexColor.parse("#80000000", 0));
		assertEquals(7, HexColor.parse("nope", 7));
	}
}
