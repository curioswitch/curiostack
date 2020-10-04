using CafeMap.Player.Services;
using GoogleApi;
using GoogleApi.Entities.Places.Search.Text.Request;
using UnityEngine.UI;
using Zenject;

public class SearchService
{
    private readonly ViewportService viewportService;

    private readonly SecretsService _secretsService;

    public SearchService(
        SecretsService secretsService, 
        ViewportService viewportService)
    {
        this._secretsService = secretsService;
        this.viewportService = viewportService;
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
            Key = _secretsService.GoogleApiKey,
            Query = query,
        };

        var response = await GooglePlaces.TextSearch.QueryAsync(request);
        foreach (var result in response.Results)
        {
            var location = result.Geometry.Location;
            viewportService.SetCenter(location.Latitude, location.Longitude);
            break;
        }
    }
}