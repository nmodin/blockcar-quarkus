package se.nmodin.blockcar.model;

public enum Location {
    STOCKHOLM("0.300001", "Stockholm"),
    UPPSALA("0.300003", "Uppsala"),
    SODERMANLAND("0.300004", "Södermanland"),
    OSTERGOTLAND("0.300005", "Östergötland"),
    JONKOPING("0.300006", "Jönköping"),
    KRONOBERG("0.300007", "Kronoberg"),
    KALMAR("0.300008", "Kalmar"),
    GOTLAND("0.300009", "Gotland"),
    BLEKINGE("0.300010", "Blekinge"),
    SKANE("0.300012", "Skåne"),
    HALLAND("0.300013", "Halland"),
    VASTRA_GOTALAND("0.300014", "Västra Götaland"),
    VARMLAND("0.300017", "Värmland"),
    OREBRO("0.300018", "Örebro"),
    VASTMANLAND("0.300019", "Västmanland"),
    DALARNA("0.300020", "Dalarna"),
    GAVLEBORG("0.300021", "Gävleborg"),
    VASTERNORRLAND("0.300022", "Västernorrland"),
    JAMTLAND("0.300023", "Jämtland"),
    VASTERBOTTEN("0.300024", "Västerbotten"),
    NORRBOTTEN("0.300025", "Norrbotten");

    private final String code;
    private final String displayName;

    Location(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Location fromName(String name) {
        String normalized = name.toUpperCase().replace("Ö", "O").replace("Å", "A").replace("Ä", "A");
        try {
            return Location.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
