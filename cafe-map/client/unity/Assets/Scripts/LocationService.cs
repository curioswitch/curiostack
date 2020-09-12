using System;
using System.Collections;
using System.Collections.Generic;
using Google.Maps;
using Google.Maps.Coord;
using UnityEngine;

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

        private void Awake()
        {
            player = GameObject.Find("Player");
            mapsService = GetComponent<MapsService>();
        }

        // Start is called before the first frame update
        IEnumerator Start()
        {
            // First, check if user has location service enabled
            if (!Input.location.isEnabledByUser)
            {
                Debug.Log("Location disabled");
                yield break;
            }

            // Start service before querying location
            Input.location.Start();

            // Wait until service initializes
            int maxWait = 20;
            while (Input.location.status == LocationServiceStatus.Initializing && maxWait > 0)
            {
                yield return new WaitForSeconds(1);
                maxWait--;
            }

            // Service didn't initialize in 20 seconds
            if (maxWait < 1)
            {
                Debug.Log("Timed out");
                yield break;
            }

            // Connection has failed
            if (Input.location.status == LocationServiceStatus.Failed)
            {
                Debug.Log("Unable to determine device location");
                yield break;
            }

            // Access granted and location value could be retrieved
            Debug.Log("Location: " + Input.location.lastData.latitude + " " + Input.location.lastData.longitude + " " + Input.location.lastData.altitude + " " + Input.location.lastData.horizontalAccuracy + " " + Input.location.lastData.timestamp);

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
        void Stop()
        {
            Input.location.Stop();
        }
    }

}