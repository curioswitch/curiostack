using CafeMap.Map;
using CafeMap.Player.Services;
using Google.Maps;
using Google.Maps.Coord;
using GoogleApi;
using GoogleApi.Entities.Places.Search.Text.Request;
using UnityEngine.UI;
using Zenject;

public class SearchService
{
    private readonly PanAndZoom cameraControl;

    private readonly DynamicMapsUpdater mapsUpdater;

    private readonly MapsService mapsService;
    private readonly KeysService keysService;

    public SearchService(
        PanAndZoom cameraControl,
        MapsService mapsService, 
        KeysService keysService, 
        DynamicMapsUpdater mapsUpdater)
    {
        this.cameraControl = cameraControl;
        this.mapsService = mapsService;
        this.keysService = keysService;
        this.mapsUpdater = mapsUpdater;
    }

    [Inject]
    public void Init([Inject(Id = "SearchBox")] InputField searchBox)
    {
        searchBox.onEndEdit.AddListener(text => search(text));
    }
    
    private async void search(string query)
    {
        var request = new PlacesTextSearchRequest
        {
            Key = keysService.GoogleApiKey,
            Query = query,
        };

        var response = await GooglePlaces.TextSearch.QueryAsync(request);
        foreach (var result in response.Results)
        {
            var location = result.Geometry.Location;
            var latlng = new LatLng(location.Latitude, location.Longitude);
            mapsService.MoveFloatingOrigin(latlng);
            var coords = mapsService.Coords.FromLatLngToVector3(latlng);
            cameraControl.SetPosition(coords);
            mapsUpdater.LoadMap();
            break;
        }
    }
}