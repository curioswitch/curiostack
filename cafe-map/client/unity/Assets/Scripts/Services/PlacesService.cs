using System.Threading.Tasks;
using CafeMap.Player.Services;
using GoogleApi;
using GoogleApi.Entities.Places.Details.Request;
using GoogleApi.Entities.Places.Details.Request.Enums;
using Zenject;

namespace CafeMap.Services
{
    public class PlacesService
    {
        private readonly SecretsService _secretsService;

        [Inject]
        public PlacesService(SecretsService secretsService)
        {
            _secretsService = secretsService;
        }

        public async Task<string> refreshPlaceId(string placeId)
        {
            var request = new PlacesDetailsRequest
            {
                Key = _secretsService.GoogleApiKey,
                PlaceId = placeId,
            };

            var response = await GooglePlaces.Details.QueryAsync(request);
            return response.Result.PlaceId;
        }
    }
}