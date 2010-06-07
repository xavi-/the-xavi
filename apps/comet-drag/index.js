(function(context, undefined) {
    context.start = function(chn) {
        function left(callback) {
            var channel = chn.channels["comet-drag"];
            
            if(!channel || channel.data.length === 0) { callback(0); }
            else { callback(channel.data[channel.data.length - 1].message.content.left); }
        }
        
        function top(callback) {
            var channel = chn.channels["comet-drag"];
            
            if(!channel || channel.data.length === 0) { callback(0); }
            else { callback(channel.data[channel.data.length - 1].message.content.top); }
        }
        
        function infoId(callback) {
            var channel = chn.channels["comet-drag"];
            
            if(!channel) { callback(0); }
            else { callback(channel.lastInfoId); }
        }
        
        return { "comet-drag": { left: left, top: top, "info-id": infoId } };
    };
})(exports);