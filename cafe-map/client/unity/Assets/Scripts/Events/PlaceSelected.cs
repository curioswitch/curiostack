using Org.Curioswitch.Cafemap.Api;

namespace CafeMap.Events
{
    public class PlaceSelected
    {
        public static PlaceSelected create(Place place)
        {
            return new PlaceSelected(place);
        }

        private PlaceSelected(Place place)
        {
            Place = place;
        }

        public Place Place { get; }
    }
}
