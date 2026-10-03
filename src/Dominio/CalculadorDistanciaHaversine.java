package Dominio;

public class CalculadorDistanciaHaversine implements CalculadorDistancia {

    private static final double RADIO_TIERRA_METROS = 6371000.0;

    @Override
    public double calcularDistancia(Ubicacion u1, Ubicacion u2) {
        if (u1 == null || u2 == null) {
            return Double.MAX_VALUE;
        }

        double lat1 = Math.toRadians(u1.getLatitud());
        double lon1 = Math.toRadians(u1.getLongitud());
        double lat2 = Math.toRadians(u2.getLatitud());
        double lon2 = Math.toRadians(u2.getLongitud());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return RADIO_TIERRA_METROS * c;
    }
}
