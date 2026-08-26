package gov.tak.platform.datapackage;

import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reader to parse a preferences ({@code .pref}) entry from a data package entry.
 *
 * @since 3.0
 */
public class PreferencesReader
{
    /**
     * Parse the given XML as a TAK preferences document.
     *
     * @param preferenceXml XML to parse
     * @return Preferences
     * @throws IOException If parsing fails
     */
    public Preferences parsePreferences(String preferenceXml) throws IOException
    {
        try
        {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Preferences.class);
            return (Preferences) jaxbContext.createUnmarshaller().unmarshal(new StringReader(preferenceXml));
        } catch (JAXBException e)
        {
            throw new IOException("Failed to parse preferences", e);
        }
    }

    /**
     * Top-level class for JAXB to use to parse the content.
     */
    @XmlRootElement
    public static class Preferences
    {
        @XmlElement(name = "preference")
        List<Preference> preferences;

        /**
         * Find a preference entry by name.
         *
         * @param name Name of preference to find
         * @return Preference with given name, or {@code null} if not found
         */
        @Nullable
        public Preference findByName(String name)
        {
            return preferences.stream()
                    .filter(p -> p.name.equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String toString()
        {
            return "Preferences{" +
                    "preferences=\n" + StringUtils.join(preferences, "\n   ", null) +
                    '}';
        }
    }

    /**
     * Single preference element, with a collection of "entries".
     */
    public static class Preference
    {
        @XmlAttribute
        String version;

        @XmlAttribute
        String name;

        @XmlElement(name = "entry")
        List<Entry> entries = new ArrayList<>();

        /**
         * @return A Map containing each entry value, keyed by entry name
         */
        public Map<String, String> toMap()
        {
            return entries.stream().collect(Collectors.toMap(e -> e.key, e -> e.value));
        }

        @Override
        public String toString()
        {
            return "Preference{" +
                    "version='" + version + '\'' +
                    ", name='" + name + '\'' +
                    ", entries=\n      " + StringUtils.join(entries, "\n      ", null) +
                    '}';
        }
    }

    /**
     * A singe preference entry.
     */
    public static class Entry
    {
        @XmlAttribute
        String key;

        @XmlAttribute(name = "class")
        String cls;

        @XmlValue
        String value;

        @Override
        public String toString()
        {
            return "Entry{" +
                    "key='" + key + '\'' +
                    ", cls='" + cls + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
