import SwiftUI
import fints4k

class UrlSessionWebClient : IWebClient {
    
    func post(url: String, body: String, contentType: String, userAgent: String, callback: @escaping (WebClientResponse) -> Void) {
        let request = requestFor(url, "POST", body)
        
        executeRequestAsync(request, callback)
    }
    
    func getAsync(_ url: String, callback: @escaping (WebClientResponse) -> Void) {
        let request = requestFor(url, "GET")
        
        executeRequestAsync(request, callback)
    }
    
    
    func get(_ url: String) -> WebClientResponse {
        let request = requestFor(url, "GET")
        
        return executeRequestSynchronous(request)
    }
    
    func getData(_ url: String) -> Data? {
        let request = requestFor(url, "GET")
        
        return executeDataRequestSynchronous(request)
    }
    
    func head(_ url: String) -> WebClientResponse {
        let request = requestFor(url, "HEAD")
        
        return executeRequestSynchronous(request)
    }
    
    
    func requestFor(_ url: String, _ method: String, _ body: String? = nil) -> URLRequest {
        guard let requestUrl = URL(string: url) else { fatalError() }
        
        var request = URLRequest(url: requestUrl)
        request.httpMethod = method
        
        if let body = body {
            request.httpBody = body.data(using: String.Encoding.utf8)
        }
        
        return request
    }
    
    func executeRequestAsync(_ request: URLRequest, _ callback: @escaping (WebClientResponse) -> Void) {
        let dataTask = URLSession.shared.dataTask(with: request) { (data: Data?, response: URLResponse?, error: Error?) in
            // we have to dispatch response back to main thread as in Kotlin/Native objects can only be changed on one thread -> do all logic on main thread only network access in backbackground thread
            DispatchQueue.main.async {
                var webClientResponse = WebClientResponse(successful: false, responseCode: -1, error: KotlinException(message: error?.localizedDescription), body: nil)
            
                if let data = data, let httpResponse = response as? HTTPURLResponse {
                    webClientResponse = WebClientResponse(successful: true, responseCode: Int32(httpResponse.statusCode), error: nil, body: String(data: data, encoding: .ascii))
                }
                    
                callback(webClientResponse)
            }
        }

        dataTask.resume()
    }
    
    func executeRequestSynchronous(_ request: URLRequest) -> WebClientResponse {
        var data: Data?, urlResponse: URLResponse?, error: Error?

        let semaphore = DispatchSemaphore(value: 0)

        let dataTask = URLSession.shared.dataTask(with: request) {
            data = $0
            urlResponse = $1
            error = $2

            semaphore.signal()
        }
        
        dataTask.resume()

        _ = semaphore.wait(timeout: .distantFuture)
        
        return mapResponse(data, urlResponse, error)
    }
    
    private func mapResponse(_ data: Data?, _ response: URLResponse?, _ error: Error?) -> WebClientResponse {
//        let mappedError = error == nil ? nil : KotlinException(message: error?.localizedDescription) // TODO:
        
        if let httpResponse = response as? HTTPURLResponse {
            let statusCode = Int32(httpResponse.statusCode)
//            let isSuccessful = mappedError == nil // TODO
//                && statusCode >= 200 && statusCode <= 299
            let isSuccessful = statusCode >= 200 && statusCode <= 299
            
            if let data = data {
                return WebClientResponse(successful: isSuccessful, responseCode: statusCode, error: nil, body: String(data: data, encoding: .ascii))
            }
            else {
                return WebClientResponse(successful: isSuccessful, responseCode: statusCode, error: nil, body: nil)
            }
        }
        else {
            return WebClientResponse(successful: false, responseCode: -1, error: nil, body: nil)
        }
    }
    
    
    func executeDataRequestSynchronous(_ request: URLRequest) -> Data? {
        var data: Data?

        let semaphore = DispatchSemaphore(value: 0)

        let dataTask = URLSession.shared.dataTask(with: request) { receivedData, _, _ in
            data = receivedData

            semaphore.signal()
        }
        
        dataTask.resume()

        _ = semaphore.wait(timeout: .distantFuture)
        
        return data
    }
    
}
