

def list = [ [from:"one", count:2, percentRead:0.33], [from:"two", count:5, percentRead:0.66]]
def data = [ froms: list]

def result = Template.execute("table.ms", data)
println result