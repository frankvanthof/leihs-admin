require 'active_support/all'
require 'pry'

require 'config/database'
require 'config/factories'
require 'config/web'
require 'helpers/global'

RSpec.configure do |config|

  config.include Helpers::Global

  config.before :all do
    @spec_seed = \
      ENV['SPEC_SEED'].presence.try(:strip) || `git log -n1 --format=%T`.strip
    puts "SPEC_SEED #{@spec_seed} set env SPEC_SEED to force value"
    srand Integer(@spec_seed, 16)
  end

  config.after :all do
    puts "SPEC_SEED #{@spec_seed} set env SPEC_SEED to force value"
  end

end

