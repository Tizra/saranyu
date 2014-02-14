
### saranyu

Saranyu is a clojure library for generating authentication signatures, for interacting with restful AWS APIS.As service endpoints in AWS are using different signature mechanisms, this library provides an easy way for the user to generate the http request, without worrying about the underlying signature implementation.

You can create the API request, using the AWS reference manual and pass the request to this library, as explained below.What you get back is a request map with all the signature headers added, which you can be used with any http client to get a succesful  response from  AWS API.

### Basic Usage

First pull in the library:

```clj
(use 'saranyu.core)
```

There is only a single public method, `sign` which takes a clojure map with any combination of the following keys:

`:url` The AWS API url

`:method` The request method (Example :get :post :put or :delete, default is :get)

`:body` The request body, if required

`:content-type` The content type for the request

`:params` Query string parameters as a map of key value pairs

`:headers` Request headers as a map of key value pairs

`:key` The AWS access key

`:secret` The AWS secret

`:session-token` Only if you are using role based authentication and is an S3 request


### Using the library.

An example usage would be

```clj
(sign {:url "https://ec2.eu-west-1.amazonaws.com"
       :key aws-key
       :secret aws-secret
       :params {:Version "2013-10-01"
                :Action "CreateSecurityGroup"
                :GroupName "test"
                :GroupDescription "test"}})

```

The above method returns  a map of the request details, which you can be used with any http client to interact with the API.
The possible keys in the returned map are `:url` `:method` `:content-type` `:headers` and `:body` depending on the request you made.

### Install

Fetch saranyu from [github](https://github.com/mixradio/saranyu) or pull from clojars: `[saranyu "X.Y.Z"]`

### History

Saranyu was extracted from [cluppet](http://github.com/mixradio/cluppet)

## License

[Saranyu is released under the 3-clause license ("New BSD License" or "Modified BSD License")](https://raw.github.com/mixradio/saranyu/master/resources/LICENSE.txt).
