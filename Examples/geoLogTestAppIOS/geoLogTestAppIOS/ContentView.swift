//
//  ContentView.swift
//  geoLogTestAppIOS
//
//  Created by Mohamad Dakroub on 10/03/2021.
//

import SwiftUI
import wsmobile
import CoreLocation

struct ContentView: View {
    
    private let locationManager = CLLocationManager()
    let geolog = GeoLogger()
    
    
    init() {
        print("Hello, world!")
    }
    
    
    
    var body: some View {
        Text("Hello, world!")
            .padding()
        
        Button(action: {
            self.locationManager.requestAlwaysAuthorization()
            self.locationManager.requestWhenInUseAuthorization()
        }) {
            Text("Request authorization").padding()
        }
        Button(action: {
            geolog.log(context: (), timestamp: nil, extra: nil)
        }) {
            Text("Logger").padding()
        }
    }
    
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
