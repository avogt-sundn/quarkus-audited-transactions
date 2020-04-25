### Do not use magic numbers, use 

    import javax.ws.rs.core.Response;

     ...
       return Response.ok(fruit).status(Response.Status.CREATED).build();

### Understand Http status codes and what they mean

#### 406
    NOT_ACCEPTABLE (406)
   - is *not* for declaring the client being a horrible person sending wrong data.
   - instead it was designed to declare that ``accept`` headers received cannot be fullfilled since this service can only serve different media types.
   - better use
      
    BAD_REQUEST (400)
        
#### 500

https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.11