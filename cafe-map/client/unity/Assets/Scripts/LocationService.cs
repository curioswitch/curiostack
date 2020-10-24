using System;
using System.Collections;
using System.Collections.Generic;
using Cysharp.Threading.Tasks;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;
using Zenject;

namespace CafeMap.Player
{
    public class LocationService : MonoBehaviour
    {

        public static LatLng getLocation()
        {
            if (Input.location.status == LocationServiceStatus.Running)
            {
                return new LatLng(Input.location.lastData.latitude, Input.location.lastData.longitude);
            }

            return new LatLng(35.4611144, 139.6154113);
        }

        private MapsService mapsService;
        private GameObject player;

        private bool initialized;

        [Inject]
        public void Init(MapsService mapsService)
        {
            this.mapsService = mapsService;
        }

        private void Awake()
        {
            player = GameObject.Find("Player");
        }

        // Start is called before the first frame update
        async void Start()
        {
            // First, check if user has location service enabled
            if (!Input.location.isEnabledByUser)
            {
                Debug.Log("Location disabled");
                return;
            }

            // Start service before querying location
            Input.location.Start();

            // Wait until service initializes
            int maxWait = 20;
            while (Input.location.status == LocationServiceStatus.Initializing && maxWait > 0)
            {
                await UniTask.Delay(TimeSpan.FromSeconds(1));
                maxWait--;
            }

            // Service didn't initialize in 20 seconds
            if (maxWait < 1)
            {
                Debug.Log("Timed out");
                return;
            }

            // Connection has failed
            if (Input.location.status != LocationServiceStatus.Running)
            {
                Debug.Log("Unable to determine device location");
                return;
            }

            // Access granted and location value could be retrieved

            initialized = true;
        }

        void Update()
        {
            if (!initialized)
            {
                return;
            }
            var location = Input.location.lastData;
            player.transform.position = mapsService.Coords.FromLatLngToVector3(new LatLng(location.latitude, location.longitude));
        }

        // Update is called once per frame
        void OnDestroy()
        {
            Input.location.Stop();
        }
    }

}
